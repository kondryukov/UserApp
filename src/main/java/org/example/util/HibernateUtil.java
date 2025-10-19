package org.example.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.domain.User;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.SessionFactory;

public final class HibernateUtil {
    private static final SessionFactory SESSION_FACTORY;
    private static final Logger log = LogManager.getLogger(HibernateUtil.class);

    static {
        try {
            log.info("Initialisation SessionFactory...");
            StandardServiceRegistry registry = new org.hibernate.boot.registry.StandardServiceRegistryBuilder()
                    .configure()
                    .build();
            SESSION_FACTORY = new org.hibernate.boot.MetadataSources(registry)
                    .addAnnotatedClass(User.class)
                    .buildMetadata()
                    .buildSessionFactory();
            log.info("SessionFactory successfully created.");
        } catch (RuntimeException ex) {
            LogManager.getLogger(HibernateUtil.class)
                    .fatal("Initialisation of SessionFactory is failed", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private HibernateUtil() {
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }
}
