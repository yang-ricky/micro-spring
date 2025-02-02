import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// 处理 __dirname 在 ES 模块中的替代方案
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 配置选项
const config = {
    isAll: true,
    // 要排除的目录名称数组
    excludedDirs: ['micro-spring-example', 'test'],
    // 只包含的目录路径数组（相对于根目录）
    onlyIncludeDirs: ['micro-spring-web','micro-spring-context','micro-spring-core'],
    // 要搜索的类名（当 isAll 为 false 时使用）
    className: [
        "BeanDefinition",
        "BeanFactory",
        "DefaultBeanFactory",
        "DefaultBeanDefinition",
        "XmlBeanDefinitionReader",
        "BeanDefinitionHolder",
    ]
};

// 定义要搜索的根目录
const rootDir = path.resolve(__dirname);

// 定义输出文件的路径
const outputFilePath = path.join(rootDir, 'output.txt');

// 初始化或清空输出文件
await fs.writeFile(outputFilePath, '', 'utf8');

/**
 * 移除 Java 代码中的注释
 * @param {string} code - Java 源代码
 * @returns {string} - 移除注释后的代码
 */
function removeComments(code) {
    // 移除多行注释 /* ... */
    code = code.replace(/\/\*[\s\S]*?\*\//g, '');
    
    // 移除单行注释 //
    code = code.replace(/\/\/.*$/gm, '');
    
    // 移除空行
    code = code.replace(/^\s*[\r\n]/gm, '');
    
    return code;
}

/**
 * 移除方法体，只保留方法签名的花括号
 * @param {string} code - Java 源代码
 * @returns {string} - 只保留方法签名的代码
 */
function removeMethodBodies(code) {
    // 一个简单的正则，用来找到"方法定义的开头"位置
    const methodRegex = /(\b(public|protected|private|static|final|native|synchronized|abstract|transient|\s)*[\w\<\>\[\]]+\s+[\w\$]+\s*\([^\)]*\)\s*(?:throws\s+[^{]+)?\{)/g;

    let result = '';
    let lastIndex = 0;

    // exec循环，用于逐个匹配方法
    let match;
    while ((match = methodRegex.exec(code)) !== null) {
        // 把方法签名之前的代码，先加到 result
        result += code.substring(lastIndex, match.index);
        
        // match[1] 是方法签名 + '{'
        const methodSignature = match[1];

        // 找到方法体结尾的 '}'
        let braceCount = 1;
        let startIndex = match.index + methodSignature.length;
        let endIndex = startIndex;

        // 向后遍历字符，直到 braceCount 回到 0
        for (let i = startIndex; i < code.length; i++) {
            if (code[i] === '{') {
                braceCount++;
            } else if (code[i] === '}') {
                braceCount--;
                if (braceCount === 0) {
                    endIndex = i;
                    break;
                }
            }
        }

        // 把"方法签名 + 空实现"拼接
        let trimmedSignature = methodSignature.trimEnd();
        if (trimmedSignature.endsWith('{')) {
            trimmedSignature = trimmedSignature.slice(0, -1).trimEnd();
        }
        result += trimmedSignature + " {\n}\n";

        // 更新 lastIndex
        lastIndex = endIndex + 1;
    }

    // 处理剩余的代码段
    result += code.substring(lastIndex);

    return result;
}

/**
 * 检查目录是否在包含列表中
 * @param {string} dirPath - 目录路径
 * @returns {boolean} - 是否在包含列表中
 */
function isInIncludedDirectories(dirPath) {
    // 如果没有指定包含目录，则认为所有目录都可以包含
    if (!config.onlyIncludeDirs || config.onlyIncludeDirs.length === 0) {
        return true;
    }

    // 检查当前目录是否在包含列表中
    return config.onlyIncludeDirs.some(includedDir => 
        dirPath.includes(path.sep + includedDir) || 
        dirPath.endsWith(includedDir)
    );
}

/**
 * 检查目录是否应该被排除
 * @param {string} dirPath - 目录路径
 * @returns {boolean} - 是否应该排除该目录
 */
function shouldExcludeDirectory(dirPath) {
    // 首先检查是否在包含列表中
    if (!isInIncludedDirectories(dirPath)) {
        return true;
    }

    // 然后检查是否在排除列表中
    return config.excludedDirs.some(excludedDir => 
        dirPath.includes(path.sep + excludedDir) || 
        dirPath.endsWith(excludedDir)
    );
}

/**
 * 检查文件是否应该被处理
 * @param {string} filePath - 文件路径
 * @param {string} fileName - 文件名
 * @returns {boolean} - 是否应该处理该文件
 */
function shouldProcessFile(filePath, fileName) {
    // 检查文件是否在排除目录中
    if (shouldExcludeDirectory(filePath)) {
        return false;
    }

    // 排除测试相关文件
    if (filePath.includes('/test/') ||    // 测试目录
        fileName.endsWith('Test.java') ||  // 测试类
        fileName.startsWith('Test')) {     // Test开头的类
        return false;
    }

    if (config.isAll) {
        return true;
    }

    // 当 isAll 为 false 时，使用类名匹配逻辑
    return config.className.some(name => fileName.includes(name));
}

/**
 * 递归遍历目录并处理文件
 * @param {string} dir - 当前目录路径
 */
async function traverseDirectory(dir) {
    try {
        const filesAndDirs = await fs.readdir(dir, { withFileTypes: true });

        for (const item of filesAndDirs) {
            const fullPath = path.join(dir, item.name);

            if (item.isDirectory()) {
                // 检查是否应该排除该目录
                if (shouldExcludeDirectory(fullPath)) {
                    console.log(`跳过排除的目录: ${fullPath}`);
                    continue;
                }
                // 递归遍历子目录
                await traverseDirectory(fullPath);
            } else if (item.isFile()) {
                const fileExtension = path.extname(item.name).toLowerCase();

                // 仅处理 .java 文件
                if (fileExtension !== '.java') {
                    continue;
                }

                // 使用新的文件过滤逻辑
                if (shouldProcessFile(fullPath, item.name)) {
                    try {
                        // 读取文件内容
                        const data = await fs.readFile(fullPath, 'utf8');
                        // 先移除注释，再移除方法体
                        let cleanedData = removeComments(data);
                        cleanedData = removeMethodBodies(cleanedData);
                        await fs.appendFile(outputFilePath, `\n\n=== ${fullPath} ===\n\n`, 'utf8');
                        await fs.appendFile(outputFilePath, cleanedData, 'utf8');
                        console.log(`已合并文件: ${fullPath}`);
                    } catch (err) {
                        console.error(`读取或写入文件时出错: ${fullPath}`, err);
                    }
                }
            }
        }
    } catch (err) {
        console.error(`遍历目录时出错: ${dir}`, err);
    }
}

// 开始遍历
await traverseDirectory(rootDir);

console.log(`所有匹配的 .java 文件已合并到 ${outputFilePath}`);
