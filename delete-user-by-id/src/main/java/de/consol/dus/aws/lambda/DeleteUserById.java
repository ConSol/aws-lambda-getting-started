package de.consol.dus.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import de.consol.dus.aws.lambda.model.User;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * A class representing the Lambda to delete an existing {@link User} from database.
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
public class DeleteUserById implements RequestHandler<Long, Void> {

  private static SessionFactory sessionFactory;

  /**
   * Handler method to delete a {@link User}.
   *
   * @param id
   *    The id of the user to delete.
   * @param context
   *    Context of AWS.
   *
   * @return
   *    always {@code null}.
   * @throws NoSuchUserException
   *    If no user with the given ID exists.
   */
  @Override
  public Void handleRequest(Long id, Context context) {
    EntityManager manager = sessionFactory.createEntityManager();
    manager.getTransaction().begin();
    try {
      Optional<User> fetched = Optional.ofNullable(manager.find(User.class, id));
      fetched.ifPresent(manager::remove);
      manager.getTransaction().commit();
      fetched.ifPresent(u ->
          context
              .getLogger()
              .log(String.format(
                  "deletedUser={name=%s, email=%s}",
                  u.getName(),
                  u.getEmail())));
      fetched.orElseThrow(() -> new NoSuchUserException(id));
    } catch (NoResultException e) {
      context.getLogger().log(String.format("User with id %d not found", id.intValue()));
      manager.getTransaction().rollback();
    } finally {
      manager.close();
    }

    return null;
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