
package com.example.booknote.config;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class LuceneConfig {
    
    @Value("${lucene.index-path}")
    private String indexPath;
    
    @Bean
    public Directory directory() throws IOException {
        Path path = Paths.get(indexPath);
        return FSDirectory.open(path);
    }
    
    @Bean
    public Analyzer analyzer() {
        return new SmartChineseAnalyzer();
    }
}
