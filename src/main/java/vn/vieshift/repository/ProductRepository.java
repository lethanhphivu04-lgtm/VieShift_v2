package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderBySoldCountDesc();
    List<Product> findByActiveTrueAndCategoryOrderBySoldCountDesc(String category);
    List<Product> findByActiveTrueAndRelatedServiceCategoryOrderBySoldCountDesc(String relatedServiceCategory);
    boolean existsByName(String name);
}
