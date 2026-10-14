package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.modal.Customer;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.repository.CustomerRepository;
import com.zosh.repository.StoreRepository;
import com.zosh.service.CustomerService;
import com.zosh.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;

    @Override
    public Customer createCustomer(Customer customer) throws UserException {
        User currentUser = userService.getCurrentUser();
        Store store = getStoreForUser(currentUser);
        customer.setStore(store);
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(Long id, Customer customerData) throws ResourceNotFoundException, UserException {
        User currentUser = userService.getCurrentUser();
        Store store = getStoreForUser(currentUser);

        Customer customer = customerRepository.findByIdAndStoreId(id, store.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));

        customer.setFullName(customerData.getFullName());
        customer.setEmail(customerData.getEmail());
        customer.setPhone(customerData.getPhone());

        return customerRepository.save(customer);
    }

    @Override
    public void deleteCustomer(Long id) throws ResourceNotFoundException, UserException {
        User currentUser = userService.getCurrentUser();
        Store store = getStoreForUser(currentUser);

        Customer customer = customerRepository.findByIdAndStoreId(id, store.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));

        customerRepository.delete(customer);
    }

    @Override
    public Customer getCustomerById(Long id) throws ResourceNotFoundException, UserException {
        User currentUser = userService.getCurrentUser();
        Store store = getStoreForUser(currentUser);

        return customerRepository.findByIdAndStoreId(id, store.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }

    @Override
    public List<Customer> getAllCustomers() throws UserException {
        User currentUser = userService.getCurrentUser();
        Store store = getStoreForUser(currentUser);

        return customerRepository.findByStoreId(store.getId());
    }

    @Override
    public List<Customer> searchCustomer(String keyword) throws UserException {
        User currentUser = userService.getCurrentUser();
        Store store = getStoreForUser(currentUser);

        if (keyword == null || keyword.trim().isEmpty()) {
            return customerRepository.findByStoreId(store.getId());
        }

        return customerRepository.searchByStoreAndKeyword(store.getId(), keyword.trim());
    }

    private Store getStoreForUser(User user) throws UserException {
        if (user == null) {
            throw new UserException("User not found");
        }

        Store store = null;
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN) {
            store = user.getStore();
            if (store == null) {
                store = storeRepository.findByStoreAdminId(user.getId());
            }
        } else if (user.getRole() == UserRole.ROLE_STORE_MANAGER) {
            store = user.getStore();
            if (store == null) {
                store = storeRepository.findByStoreAdminId(user.getId());
            }
        } else {
            // Branch employees (Branch Admin, Branch Manager, Branch Cashier, etc.)
            if (user.getBranch() != null) {
                store = user.getBranch().getStore();
            }
            if (store == null) {
                store = user.getStore();
            }
            if (store == null) {
                store = storeRepository.findByStoreAdminId(user.getId());
            }
        }

        if (store == null) {
            throw new UserException("Store not found for the current user");
        }
        return store;
    }

}

