package org.jumpmind.metl.core.runtime.resource;

public class FileInfo {

    long size;
    String relativePath;
    boolean directory;
    long lastUpdated;
    String name;
    
    public FileInfo(String path, boolean directory, long lastUpdated, long size) {
        this.relativePath = path;
        this.directory = directory;
        this.lastUpdated = lastUpdated;
        this.size = size;
        int index = relativePath.lastIndexOf("/");
        if (index > 0) {
            name = relativePath.substring(index + 1, relativePath.length());
        } else {
            name = relativePath;
        }

    }
    
    public long getSize() {
        return size;
    }
    
    public String getName() {
        return name;
    }
    
    public String getRelativePath() {
        return relativePath;
    }
    
    public boolean isDirectory() {
        return directory;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    @Override
    public String toString() {
        return relativePath;
    }

}
