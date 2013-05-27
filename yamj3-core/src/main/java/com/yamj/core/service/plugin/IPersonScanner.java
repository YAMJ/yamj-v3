package com.yamj.core.service.plugin;

import com.yamj.core.database.model.Person;

public interface IPersonScanner extends IPluginDatabaseScanner {

    String getPersonId(Person person);

    String getPersonId(String name);

    ScanResult scan(Person person);
}
