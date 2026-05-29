package ewm.event.service;

import ewm.interaction.dto.category.CategoryDto;
import ewm.interaction.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);

    CategoryDto update(Long id, NewCategoryDto dto);

    void delete(Long id);

    List<CategoryDto> findAll(int from, int size);

    CategoryDto findById(Long id);
}
