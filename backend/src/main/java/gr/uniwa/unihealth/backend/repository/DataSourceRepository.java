package gr.uniwa.unihealth.backend.repository;

import gr.uniwa.unihealth.backend.model.DataSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends BaseRepository<DataSource> {

  List<DataSource> findByActiveTrueOrderByDisplayOrderAscNameAsc();

  Optional<DataSource> findByCode(String code);
}
