package ewm.event.service;

import ewm.event.mapper.CategoryMapper;
import ewm.event.model.Category;
import ewm.event.repository.CategoryRepository;
import ewm.event.repository.DatabaseEventRepository;
import ewm.interaction.dto.category.CategoryDto;
import ewm.interaction.dto.category.NewCategoryDto;
import ewm.interaction.exception.ConflictException;
import ewm.interaction.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final DatabaseEventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ConflictException("Название категории должно быть уникальным");
        }
        Category category = CategoryMapper.toEntity(dto);
        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto update(Long id, NewCategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не была найдена"));

        if (!category.getName().equalsIgnoreCase(dto.getName())
                && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new ConflictException("Название категории должно быть уникальным");
        }

        category.setName(dto.getName());
        return CategoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не была найдена"));

        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category is not empty");
        }

        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> findAll(int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return categoryRepository.findAll(page).stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не была найдена"));
        return CategoryMapper.toDto(category);
    }
}
