package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.dto.UnihealthAssistantContentDTO;
import gr.uniwa.unihealth.backend.dto.UnihealthAssistantItemDTO;
import gr.uniwa.unihealth.backend.mapper.BaseEntityMapper;
import gr.uniwa.unihealth.backend.mapper.UnihealthAssistantItemMapper;
import gr.uniwa.unihealth.backend.model.UnihealthAssistantItem;
import gr.uniwa.unihealth.backend.model.enums.AssistantSection;
import gr.uniwa.unihealth.backend.repository.BaseRepository;
import gr.uniwa.unihealth.backend.repository.UnihealthAssistantItemRepository;
import gr.uniwa.unihealth.backend.service.UnihealthAssistantReaderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static gr.uniwa.unihealth.backend.service.bootstrap.UnihealthAssistantInitData.seedInitialData;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnihealthAssistantReaderServiceImpl
    extends BaseReaderServiceImpl<UnihealthAssistantItemDTO, UnihealthAssistantItem>
    implements UnihealthAssistantReaderService {

  private final UnihealthAssistantItemRepository repository;
  private final UnihealthAssistantItemMapper mapper;
  private final TenantContext tenantContext;

  @PostConstruct
  public void init() {
    tenantContext.runForEachTenant(tenantId -> {
      if (repository.count() == 0) {
        log.info("Initializing health assistant content for tenant [{}].", tenantId);
        seedInitialData(repository);
      } else {
        log.info(
            "Health assistant content already exists for tenant [{}]. Skipping initialization.",
            tenantId);
      }
    }, "HealthAssistantReaderServiceImpl.init");
  }


  @Override
  public UnihealthAssistantContentDTO findContent() {
    UnihealthAssistantContentDTO dto = new UnihealthAssistantContentDTO();

    dto.setFaq(
        map(repository.findByActiveTrueAndSectionOrderByDisplayOrderAsc(AssistantSection.FAQ)));
    dto.setServices(map(repository.findByActiveTrueAndSectionOrderByDisplayOrderAsc(
        AssistantSection.SERVICES)));
    dto.setEmergency(map(repository.findByActiveTrueAndSectionOrderByDisplayOrderAsc(
        AssistantSection.EMERGENCY)));
    dto.setClinics(
        map(repository.findByActiveTrueAndSectionOrderByDisplayOrderAsc(AssistantSection.CLINICS)));

    return dto;
  }

  @Override
  public UnihealthAssistantContentDTO searchContent(String search) {
    if (search == null || search.isBlank()) {
      return findContent();
    }

    String searchToUse = search.trim();

    UnihealthAssistantContentDTO dto = new UnihealthAssistantContentDTO();
    dto.setFaq(map(repository.searchBySection(AssistantSection.FAQ, searchToUse)));
    dto.setServices(map(repository.searchBySection(AssistantSection.SERVICES, searchToUse)));
    dto.setEmergency(map(repository.searchBySection(AssistantSection.EMERGENCY, searchToUse)));
    dto.setClinics(map(repository.searchBySection(AssistantSection.CLINICS, searchToUse)));

    return dto;
  }

  private List<UnihealthAssistantItemDTO> map(List<UnihealthAssistantItem> items) {
    return items.stream().map(mapper::mapToDTO).toList();
  }

  @Override
  protected BaseEntityMapper<UnihealthAssistantItemDTO, UnihealthAssistantItem> getMapper() {
    return mapper;
  }

  @Override
  protected BaseRepository<UnihealthAssistantItem> getRepository() {
    return repository;
  }
}
