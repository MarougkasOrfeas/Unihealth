package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.dto.DataSourceDTO;

import java.util.List;

public interface DataSourceReaderService extends BaseReaderService<DataSourceDTO> {

  /**
   * The sources the content came from, for the credits shown alongside it.
   *
   * @return The active sources in display order.
   */
  List<DataSourceDTO> findContent();
}
