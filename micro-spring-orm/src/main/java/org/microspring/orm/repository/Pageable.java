package org.microspring.orm.repository;

public interface Pageable {
    int getPageNumber();
    int getPageSize();
    Sort getSort();
    
    static Pageable of(int page, int size) {
        return new PageRequest(page, size);
    }
    
    static Pageable of(int page, int size, Sort sort) {
        return new PageRequest(page, size, sort);
    }
}

class PageRequest implements Pageable {
    private final int page;
    private final int size;
    private final Sort sort;
    
    PageRequest(int page, int size) {
        this(page, size, null);
    }
    
    PageRequest(int page, int size, Sort sort) {
        this.page = page;
        this.size = size;
        this.sort = sort;
    }
    
    @Override
    public int getPageNumber() { return page; }
    
    @Override
    public int getPageSize() { return size; }
    
    @Override
    public Sort getSort() { return sort; }
} 