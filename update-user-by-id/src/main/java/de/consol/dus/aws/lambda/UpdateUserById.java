package de.consol.dus.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import de.consol.dus.aws.lambda.model.User;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * A class representing the Lambda to update an existing {@link User} from database by id .
 * <br>
 *
 * Database connection is configured through the following environment variables:
 * <ul>
 * <li><code>RDS_ENDPOINT</code>: the URI to the database service</li>
 * <li><code>RDS_DB_NAME</code>: the name of the database to use</li>
 * <li><code>RDS_USERNAME</code>: username to access the database</li>
 * <li><code>RDS_PASSWORD</code>: password to access the database</li>
 * </ul>
 *
 * @author Marco Bungart
 */
public class UpdateUserById implements RequestHandler<UpdateUserRequest, Void> {

  private static SessionFactory sessionFactory;

  /**
   * Handler method to update an existing {@link User}.
   *
   * @param updateRequest
   *    The {@link UpdateUserRequest}, representing the changes to the user.
   * @param context
   *    Context of AWS.
   *
   * @return
   *    always {@code null}.
   */
  @Override
  public Void handleRequest(UpdateUserRequest updateRequest, Context context) {
    EntityManager manager = sessionFactory.createEntityManager();
    CriteriaBuilder builder = manager.getCriteriaBuilder();
    CriteriaUpdate<User> update = builder.createCriteriaUpdate(User.class);
    Root<User> root = update.from(User.class);
    context.getLogger().log(String.format("updating user %d", updateRequest.getId()));

    updateRequest.getName().ifPresent(n -> {
      update.set(root.get("name"), n);
      context.getLogger().log(String.format("updating name to %s%n", n));
    });

    updateRequest.getEmail().ifPresent(e -> {
      update.set(root.get("email"), e);
      context.getLogger().log(String.format("updating email to %s%n", e));
    });

    update.where(builder.equal(root.get("id"), updateRequest.getId()));

    try {
      manager.getTransaction();
      manager.getTransaction().begin();
      int numUpdated = manager.createQuery(update).executeUpdate();
      context
          .getLogger()
          .log(String.format(
              "Updated %d entries",
              numUpdated));
      manager.getTransaction().commit();
    } catch (Exception e) {
      manager.getTransaction().rollback();
      throw e;
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