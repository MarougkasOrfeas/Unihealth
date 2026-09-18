package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.service.impl.VirusScanServiceImpl.InputStreamSupplier;

public interface VirusScanService {

  void scanForViruses(InputStreamSupplier virusScanSupplier);
}
