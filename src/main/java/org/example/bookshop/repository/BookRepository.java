package org.example.bookshop.repository;

import org.example.bookshop.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Page<Book> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE (:q IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:categoryId IS NULL OR b.category.id = :categoryId)
              AND (:minPrice IS NULL OR b.price >= :minPrice)
              AND (:maxPrice IS NULL OR b.price <= :maxPrice)
            """)
    Page<Book> search(@Param("q") String q,
                      @Param("categoryId") Long categoryId,
                      @Param("minPrice") BigDecimal minPrice,
                      @Param("maxPrice") BigDecimal maxPrice,
                      Pageable pageable);
}
