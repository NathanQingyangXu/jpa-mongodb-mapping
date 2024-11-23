package org.hibernate.omm.data;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "Book")
public class Book {

  @Id
  long id;

  String title;

  public Book() {}

  public Book(long id) {
    this(id, null);
  }

  public Book(long id, String title) {
    this.id = id;
    this.title = title;
  }
}
