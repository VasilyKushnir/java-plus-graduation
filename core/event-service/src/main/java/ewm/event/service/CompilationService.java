package ewm.event.service;

import ewm.interaction.dto.compilation.CompilationDto;
import ewm.interaction.dto.compilation.NewCompilationDto;
import ewm.interaction.dto.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {


    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(Long compId, UpdateCompilationRequest dto);

    void delete(Long compId);

    List<CompilationDto> findAll(Boolean pinned, int from, int size);

    CompilationDto findById(Long compId);
}
