package com.zosh.service;


import com.zosh.exception.ResourceNotFoundException;
import com.zosh.exception.UserException;
import com.zosh.modal.Customer;

import java.util.List;

public interface CustomerService {

    Customer createCustomer(Customer customer) throws UserException;

    Customer updateCustomer(Long id, Customer customer) throws ResourceNotFoundException, UserException;

    void deleteCustomer(Long id) throws ResourceNotFoundException, UserException;

    Customer getCustomerById(Long id) throws ResourceNotFoundException, UserException;

    List<Customer> getAllCustomers() throws UserException;

    List<Customer> searchCustomer(String keyword) throws UserException;

}


