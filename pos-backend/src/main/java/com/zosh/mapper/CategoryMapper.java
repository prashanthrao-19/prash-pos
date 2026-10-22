package com.zosh.mapper;

import com.zosh.modal.Category;
import com.zosh.payload.dto.CategoryDTO;

public class CategoryMapper {

    public static CategoryDTO toDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .storeId(category.getStore() != null ? category.getStore().getId() : null)
                .build();
    }
}
