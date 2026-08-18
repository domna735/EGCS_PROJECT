package com.ruoyi.migration.builder;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class TfFileWriter {

    /**
     * 建立目錄（如果不存在）
     */
    public void ensureDirectory(String dirPath) throws Exception {
        Files.createDirectories(Path.of(dirPath));
    }

    /**
     * 寫入檔案（覆寫）
     */
    public void write(String filePath, String content) throws Exception {
        FileWriter fw = new FileWriter(filePath);
        fw.write(content);
        fw.close();
    }

    /**
     * 追加內容（用於日誌或多段 TF）
     */
    public void append(String filePath, String content) throws Exception {
        FileWriter fw = new FileWriter(filePath, true);
        fw.write(content);
        fw.close();
    }

    /**
     * 建立空檔案
     */
    public void touch(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
