package com.zosh.repository;

import com.zosh.modal.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName, String email);
    List<Customer> findByStoreId(Long storeId);

    Optional<Customer> findByIdAndStoreId(Long id, Long storeId);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.store.id = :storeId
        AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    List<Customer> searchByStoreAndKeyword(@Param("storeId") Long storeId, @Param("keyword") String keyword);

//    analysis
@Query("""
        SELECT COUNT(DISTINCT o.customer.id)
        FROM Order o
        WHERE o.branch.store.storeAdmin.id = :storeAdminId
    """)
int countByStoreAdminId(@Param("storeAdminId") Long storeAdminId);
}
