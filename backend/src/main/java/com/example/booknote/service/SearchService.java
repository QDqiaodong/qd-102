
package com.example.booknote.service;

import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    
    @Autowired
    private Directory directory;
    
    @Autowired
    private Analyzer analyzer;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private NoteRepository noteRepository;
    
    @PostConstruct
    public void init() throws IOException {
        rebuildIndex();
    }
    
    public void rebuildIndex() throws IOException {
        List<Book> books = bookRepository.findAll();
        List<Note> notes = noteRepository.findAll();
        
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            writer.deleteAll();
            
            for (Book book : books) {
                Document doc = new Document();
                doc.add(new StringField("type", "book", Field.Store.YES));
                doc.add(new StringField("id", book.getId().toString(), Field.Store.YES));
                doc.add(new TextField("title", book.getTitle(), Field.Store.YES));
                doc.add(new TextField("author", book.getAuthor() != null ? book.getAuthor() : "", Field.Store.YES));
                doc.add(new TextField("description", book.getDescription() != null ? book.getDescription() : "", Field.Store.YES));
                writer.addDocument(doc);
            }
            
            for (Note note : notes) {
                Document doc = new Document();
                doc.add(new StringField("type", "note", Field.Store.YES));
                doc.add(new StringField("id", note.getId().toString(), Field.Store.YES));
                doc.add(new TextField("title", note.getTitle(), Field.Store.YES));
                doc.add(new TextField("content", note.getContent() != null ? note.getContent() : "", Field.Store.YES));
                writer.addDocument(doc);
            }
        }
    }
    
    public List<SearchResult> search(String keyword) throws IOException {
        List<SearchResult> results = new ArrayList<>();
        
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            String[] fields = {"title", "author", "description", "content"};
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            
            Query query;
            try {
                query = parser.parse(QueryParser.escape(keyword));
            } catch (ParseException e) {
                throw new IOException("Failed to parse search keyword", e);
            }
            TopDocs topDocs = searcher.search(query, 50);
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                SearchResult result = new SearchResult();
                result.setType(doc.get("type"));
                result.setId(Long.parseLong(doc.get("id")));
                result.setTitle(doc.get("title"));
                results.add(result);
            }
        }
        
        return results;
    }
    
    public static class SearchResult {
        private String type;
        private Long id;
        private String title;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }
}
