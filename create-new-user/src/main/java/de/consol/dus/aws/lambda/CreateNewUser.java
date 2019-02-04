package de.consol.dus.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import de.consol.dus.aws.lambda.model.User;
import javax.persistence.EntityManager;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * A class representing the Lambda to create a new {@link User} and store her/him in database.
 * <br>
 *
 * Database connection is configured through the following environment variables:
 * <ul>
 *   <li><code>RDS_ENDPOINT</code>: the URI to the database service</li>
 *   <li><code>RDS_DB_NAME</code>: the name of the database to use</li>
 *   <li><code>RDS_USERNAME</code>: username to access the database</li>
 *   <li><code>RDS_PASSWORD</code>: password to access the database</li>
 * </ul>
 *
 * @author Marco Bungart
 */
public class CreateNewUser implements RequestHandler<User, User> {

  private static SessionFactory sessionFactory;

  /**
   * Handler method to create and store a new {@link User}.
   *
   * @param newUser
   *    The new user.
   * @param context
   *    Context of AWS.
   *
   * @return
   *    The stored user.
   */
  @Override
  public User handleRequest(User newUser, Context context) {
    context
        .getLogger()
        .log(String.format(
            "newUser{name=%s, email=%s}",
            newUser.getName(),
            newUser.getEmail()));

    EntityManager manager = sessionFactory.createEntityManager();
    try {
      manager.getTransaction().begin();
      manager.persist(newUser);
      manager.getTransaction().commit();
      User persisted = manager.find(User.class, newUser.getId());
      context
          .getLogger()
          .log(String.format(
              "persisted{name=%s, email=%s}",
              persisted.getName(),
              persisted.getEmail()));
      return persisted;
    } catch (Exception e) {
      manager.getTransaction().rollback();
      context.getLogger().log(e.toString());
      throw e;
    } finally {
      manager.close();
    }
  }

  static {
    Configuration configuration = new Configuration();

    String jdbcUrl = String.format(
        "jdbc:mysql://%s/%s",
        System.getenv("RDS_ENDPOINT"),
        System.getenv("RDS_DB_NAME"));

    configuration
        .addAnnotatedClass(User.class)
        .setProperty("hibernate.connection.url", jdbcUrl)
        .setProperty("hibernate.connection.username", System.getenv("RDS_USERNAME"))
        .setProperty("hibernate.connection.password", System.getenv("RDS_PASSWORD"))
        .configure();

    ServiceRegistry serviceRegistry =
        new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();

    try {
      sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    } catch (HibernateException e) {
      System.err.println("Initial SessionFactory creation failed." + e);
      throw new ExceptionInInitializerError(e);
    }
  }
}