package org.paquitosoft.lml.util;

/**
 *  Constants of the library.
 * 
 * @author paquitosoft
 */
public class LMLConstants {

    public static final String CONNECTION_SETTINGS_FILE_NAME = "lml-conn-settings";
    
    public static final String CONNECTION_SETTINGS_DRIVER = "lml.connection.settings.db.driver";
    public static final String CONNECTION_SETTINGS_URL = "lml.connection.settings.db.url";
    public static final String CONNECTION_SETTINGS_USER = "lml.connection.settings.db.user";
    public static final String CONNECTION_SETTINGS_PASSWORD = "lml.connection.settings.db.password";
    public static final String CONNECTION_SETTINGS_POOL_SIZE = "lml.connection.settings.pool.size";

    public static final String CONNECTION_SETTING_DATASOURCE_JNDI = "lml.connection.settings.datasource.jndi";
    
    public static final byte DETAIL_LEVEL_SHALLOW = 1;
    public static final byte DETAIL_LEVEL_DEEP = 2;
    public static final byte DETAIL_LEVEL_DEEPEST = 3;

    public static final byte PERSIST_MODE_SAVE = 1;
    public static final byte PERSIST_MODE_UPDATE = 2;
    public static final byte PERSIST_MODE_DELETE = 3;
}
