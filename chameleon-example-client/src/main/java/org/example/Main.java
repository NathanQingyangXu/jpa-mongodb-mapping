package org.example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.cfg.Configuration;
import org.hibernate.omm.cfg.MongoAvailableSettings;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: Main <mongo_connection_url> <mongo_database>");
            System.exit(1);
        }
        var cfg = new Configuration();

        cfg.setProperty(MongoAvailableSettings.MONGODB_CONNECTION_URL.getConfiguration(), args[0]);
        cfg.setProperty(MongoAvailableSettings.MONGODB_DATABASE.getConfiguration(), args[1]);

        cfg.addAnnotatedClass(Book.class);

        try (var sessionFactory = cfg.buildSessionFactory()) {
            sessionFactory.inTransaction(session -> {
                var book = new Book();
                book.id = 1;
                book.title = "Thinking in Java";
                session.persist(book);
            });
        }
    }

    @Entity(name = "Book")
    @Table(name = "books")
    static class Book {
        @Id
        int id;
        String title;
    }
}
