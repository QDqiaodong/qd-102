
package com.example.booknote.service;

import com.example.booknote.entity.Book;
import com.example.booknote.entity.Note;
import com.example.booknote.entity.Tag;
import com.example.booknote.repository.BookRepository;
import com.example.booknote.repository.NoteRepository;
import com.example.booknote.repository.TagRepository;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    
    @Autowired
    private TagRepository tagRepository;
    
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
                doc.add(new StringField("status", book.getStatus() != null ? book.getStatus().name() : "", Field.Store.YES));
                doc.add(new StringField("category", book.getCategory() != null ? book.getCategory() : "", Field.Store.YES));
                doc.add(new LongPoint("createdAt", book.getCreatedAt() != null ? 
                    book.getCreatedAt().toEpochSecond(ZoneOffset.UTC) : 0L));
                doc.add(new LongPoint("updatedAt", book.getUpdatedAt() != null ? 
                    book.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) : 0L));
                doc.add(new StringField("createdAtStr", book.getCreatedAt() != null ? 
                    book.getCreatedAt().toString() : "", Field.Store.YES));
                doc.add(new StringField("updatedAtStr", book.getUpdatedAt() != null ? 
                    book.getUpdatedAt().toString() : "", Field.Store.YES));
                writer.addDocument(doc);
            }
            
            for (Note note : notes) {
                Document doc = new Document();
                doc.add(new StringField("type", "note", Field.Store.YES));
                doc.add(new StringField("id", note.getId().toString(), Field.Store.YES));
                doc.add(new StringField("bookId", note.getBook() != null ? note.getBook().getId().toString() : "", Field.Store.YES));
                doc.add(new TextField("title", note.getTitle(), Field.Store.YES));
                String tagNames = note.getTags() != null ? 
                    note.getTags().stream().map(Tag::getName).collect(Collectors.joining(" ")) : "";
                doc.add(new TextField("tags", tagNames, Field.Store.YES));
                String tagIds = note.getTags() != null ? 
                    note.getTags().stream().map(t -> t.getId().toString()).collect(Collectors.joining(" ")) : "";
                doc.add(new StringField("tagIds", tagIds, Field.Store.NO));
                doc.add(new TextField("content", note.getContent() != null ? note.getContent() : "", Field.Store.YES));
                doc.add(new LongPoint("createdAt", note.getCreatedAt() != null ? 
                    note.getCreatedAt().toEpochSecond(ZoneOffset.UTC) : 0L));
                doc.add(new LongPoint("updatedAt", note.getUpdatedAt() != null ? 
                    note.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) : 0L));
                doc.add(new StringField("createdAtStr", note.getCreatedAt() != null ? 
                    note.getCreatedAt().toString() : "", Field.Store.YES));
                doc.add(new StringField("updatedAtStr", note.getUpdatedAt() != null ? 
                    note.getUpdatedAt().toString() : "", Field.Store.YES));
                if (note.getBook() != null) {
                    Book book = note.getBook();
                    doc.add(new StringField("bookStatus", book.getStatus() != null ? book.getStatus().name() : "", Field.Store.NO));
                    doc.add(new StringField("bookCategory", book.getCategory() != null ? book.getCategory() : "", Field.Store.NO));
                }
                writer.addDocument(doc);
            }
        }
    }
    
    public List<SearchResult> search(String keyword) throws IOException {
        return advancedSearch(keyword, null, null, null, null, null, null);
    }

    public AdvancedSearchResult advancedSearch(String keyword, String status, String category, 
                                                List<Long> tagIds, LocalDate startDate, LocalDate endDate,
                                                String searchType) throws IOException {
        AdvancedSearchResult result = new AdvancedSearchResult();
        
        List<Long> luceneBookIds = new ArrayList<>();
        List<Long> luceneNoteIds = new ArrayList<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<SearchResult> luceneResults = searchFromLucene(keyword);
            for (SearchResult sr : luceneResults) {
                if ("book".equals(sr.getType())) {
                    luceneBookIds.add(sr.getId());
                } else if ("note".equals(sr.getType())) {
                    luceneNoteIds.add(sr.getId());
                }
            }
        }
        
        List<Book> filteredBooks = filterBooks(luceneBookIds, status, category, startDate, endDate, keyword != null && !keyword.trim().isEmpty());
        List<Note> filteredNotes = filterNotes(luceneNoteIds, status, category, tagIds, startDate, endDate, keyword != null && !keyword.trim().isEmpty());
        
        if ("book".equals(searchType)) {
            filteredNotes = new ArrayList<>();
        } else if ("note".equals(searchType)) {
            filteredBooks = new ArrayList<>();
        }
        
        result.setBooks(filteredBooks);
        result.setNotes(filteredNotes);
        
        return result;
    }

    private List<SearchResult> searchFromLucene(String keyword) throws IOException {
        List<SearchResult> results = new ArrayList<>();
        
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            String[] fields = {"title", "author", "description", "content", "tags"};
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            
            Query query;
            try {
                query = parser.parse(QueryParser.escape(keyword));
            } catch (ParseException e) {
                throw new IOException("Failed to parse search keyword", e);
            }
            TopDocs topDocs = searcher.search(query, 200);
            
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

    @SuppressWarnings("unchecked")
    private List<Book> filterBooks(List<Long> bookIds, String status, String category, 
                                    LocalDate startDate, LocalDate endDate, boolean hasKeywordFilter) {
        return bookRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (hasKeywordFilter && !bookIds.isEmpty()) {
                predicates.add(root.get("id").in(bookIds));
            } else if (hasKeywordFilter && bookIds.isEmpty()) {
                predicates.add(cb.isFalse(cb.literal(true)));
            }
            
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), Book.ReadingStatus.valueOf(status)));
            }
            
            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            
            if (startDate != null) {
                LocalDateTime start = startDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            
            if (endDate != null) {
                LocalDateTime end = endDate.plusDays(1).atStartOfDay();
                predicates.add(cb.lessThan(root.get("createdAt"), end));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    @SuppressWarnings("unchecked")
    private List<Note> filterNotes(List<Long> noteIds, String bookStatus, String bookCategory,
                                    List<Long> tagIds, LocalDate startDate, LocalDate endDate, boolean hasKeywordFilter) {
        return noteRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (hasKeywordFilter && !noteIds.isEmpty()) {
                predicates.add(root.get("id").in(noteIds));
            } else if (hasKeywordFilter && noteIds.isEmpty()) {
                predicates.add(cb.isFalse(cb.literal(true)));
            }
            
            if (bookStatus != null && !bookStatus.isEmpty()) {
                predicates.add(cb.equal(root.get("book").get("status"), Book.ReadingStatus.valueOf(bookStatus)));
            }
            
            if (bookCategory != null && !bookCategory.isEmpty()) {
                predicates.add(cb.equal(root.get("book").get("category"), bookCategory));
            }
            
            if (tagIds != null && !tagIds.isEmpty()) {
                query.distinct(true);
                predicates.add(root.join("tags").get("id").in(tagIds));
            }
            
            if (startDate != null) {
                LocalDateTime start = startDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            
            if (endDate != null) {
                LocalDateTime end = endDate.plusDays(1).atStartOfDay();
                predicates.add(cb.lessThan(root.get("createdAt"), end));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
    
    public static class SearchResult {
        private String type;
        private Long id;
        private String title;
        private String highlightedTitle;
        private String highlightedContent;
        private String highlightedTags;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getHighlightedTitle() { return highlightedTitle; }
        public void setHighlightedTitle(String highlightedTitle) { this.highlightedTitle = highlightedTitle; }
        public String getHighlightedContent() { return highlightedContent; }
        public void setHighlightedContent(String highlightedContent) { this.highlightedContent = highlightedContent; }
        public String getHighlightedTags() { return highlightedTags; }
        public void setHighlightedTags(String highlightedTags) { this.highlightedTags = highlightedTags; }
    }

    public static class AdvancedSearchResult {
        private List<Book> books = new ArrayList<>();
        private List<Note> notes = new ArrayList<>();

        public List<Book> getBooks() { return books; }
        public void setBooks(List<Book> books) { this.books = books; }
        public List<Note> getNotes() { return notes; }
        public void setNotes(List<Note> notes) { this.notes = notes; }
    }
}
