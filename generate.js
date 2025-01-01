import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// 处理 __dirname 在 ES 模块中的替代方案
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 指定要搜索的类名
const className = [
    "BeanDefinition",
    "BeanFactory",
    "DefaultBeanFactory",
]

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
 * 递归遍历目录并处理文件
 * @param {string} dir - 当前目录路径
 */
async function traverseDirectory(dir) {
    try {
        const filesAndDirs = await fs.readdir(dir, { withFileTypes: true });

        for (const item of filesAndDirs) {
            const fullPath = path.join(dir, item.name);

            if (item.isDirectory()) {
                // 忽略 node_modules 和其他你想忽略的目录
                if (item.name === 'node_modules' || item.name === '.git') {
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

                // 检查文件名是否包含任何指定的类名
                const matches = className.some(name => item.name.includes(name));

                if (matches) {
                    try {
                        // 读取文件内容
                        const data = await fs.readFile(fullPath, 'utf8');
                        // 移除注释后再写入文件
                        const cleanedData = removeComments(data);
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
