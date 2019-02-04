package de.consol.dus.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import de.consol.dus.aws.lambda.model.User;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * A class representing the Lambda to fetch all users from database.
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
public class GetAllUsers implements RequestHandler<Void, List<User>> {

  private static SessionFactory sessionFactory;

  /**
   * Handler method to get all users as a {@link List}{@code <}{@link User}{@code >}.
   *
   * @param input
   *    Unused. The handler does not take any input.
   * @param context
   *    Context of AWS.
   *
   * @return all users.
   */
  @Override
  public List<User> handleRequest(Void input, Context context) {
    EntityManager manager = sessionFactory.createEntityManager();
    CriteriaBuilder builder = manager.getCriteriaBuilder();
    CriteriaQuery<User> getAll = builder.createQuery(User.class);
    Root<User> root = getAll.from(User.class);

    getAll
        .select(root)
        .orderBy(builder.asc(root.get("id")));

    List<User> users = manager.createQuery(getAll).getResultList();
    manager.close();

    context
        .getLogger()
        .log(String.format(
            "returning %d users",
            users.size()));
    return users;
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