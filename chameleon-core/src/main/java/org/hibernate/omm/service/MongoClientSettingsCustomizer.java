package org.hibernate.omm.service;

import org.hibernate.service.Service;
import com.mongodb.MongoClientSettings;

public interface MongoClientSettingsCustomizer extends Service {
  void contribute(MongoClientSettings.Builder builder);
}
