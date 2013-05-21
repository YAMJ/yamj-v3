package com.yamj.core.service.plugin;

import com.yamj.core.database.model.Person;

public interface IPersonScanner extends IPluginDatabaseScanner {

    public String getPersonId(Person person);

    public String getPersonId(String name);

    public ScanResult scan(Person person);
}
