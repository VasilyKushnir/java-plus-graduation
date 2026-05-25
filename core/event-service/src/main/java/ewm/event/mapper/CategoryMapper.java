package ewm.event.mapper;

import ewm.event.model.Category;
import ewm.interaction.dto.category.CategoryDto;
import ewm.interaction.dto.category.NewCategoryDto;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static Category toEntity(NewCategoryDto dto) {
        Category category = new Category();
        category.setName(dto.getName());
        return category;
    }

    public static CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    public static void update(Category category, CategoryDto dto) {
        category.setName(dto.getName());
    }
}
