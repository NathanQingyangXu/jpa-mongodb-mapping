package org.hibernate.omm.data;

import jakarta.annotation.Nullable;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import java.util.Optional;

@Repository
public interface BookRepository {

  @Insert
  void insert(Book book);

  @Update
  void update(Book book);

  @Delete
  void delete(Book book);

  @Find
  @Nullable
  Book find(long id);

  @Save
  void save(Book book);

}

