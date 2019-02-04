package de.consol.dus.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import de.consol.dus.aws.lambda.model.User;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * A class representing the Lambda to get an existing {@link User} from database by id.<br>
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
public class GetUserById implements RequestHandler<Long, User> {

  private static SessionFactory sessionFactory;

  /**
   * Handler method to get an exsiting {@link User} from database by her/his id.
   *
   * @param id
   *    The id of the user to get.
   * @param context
   *    Context of AWS.
   *
   * @return
   *    the user.
   * @throws NoSuchUserException
   *    If no user with the given ID exists.
   */
  @Override
  public User handleRequest(Long id, Context context) {
    EntityManager manager = sessionFactory.createEntityManager();
    Optional<User> result = Optional.ofNullable(manager.find(User.class, id));
    manager.close();
    result.orElseThrow(() -> new NoSuchUserException(id));
    return result.get();
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