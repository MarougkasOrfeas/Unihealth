package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import gr.uniwa.unihealth.backend.service.VirusScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.ClamavException;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VirusScanServiceImpl implements VirusScanService {

  private final ClamavClient clamavClient;

  @Override
  public void scanForViruses(InputStreamSupplier virusScanSupplier) {
    try (InputStream fileStream = virusScanSupplier.get()) {
      ScanResult scanResult = clamavClient.scan(fileStream);
      if (scanResult instanceof ScanResult.VirusFound) {
        Map<String, Collection<String>> viruses =
            ((ScanResult.VirusFound) scanResult).getFoundViruses();
        log.error("Viruses found: {}", viruses);
        throw new UNIHEALTHException("global.import.file.virus.found",
            "The input stream contains viruses", null);
      }
    } catch (IOException e) {
      throw new UNIHEALTHException("global.import.file.unreadable",
          "The input stream cannot be read", e);
    } catch (ClamavException clamavException) {
      throw new UNIHEALTHException("global.import.file.scan.failed", "Virus scan failed",
          clamavException);
    }
  }

  public static interface InputStreamSupplier {
    InputStream get() throws IOException;
  }
}
