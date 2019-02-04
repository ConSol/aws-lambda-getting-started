package de.consol.dus.aws.lambda.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The User POJO.
 *
 * @author Marco Bungart
 */
@Entity
@Table(name = "user")
public class User {
  private long id;
  private String name;
  private String email;

  /*
   * Added for JPA
   */
  protected User() {}

  private User(long id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user")
  public long getId() {
    return id;
  }

  /*
   * Added for JPA
   */
  private void setId(long id) {
    this.id = id;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  /*
   * Added for JPA
   */
  private void setName(String name) {
    this.name = name;
  }

  @Column(name = "email")
  public String getEmail() {
    return email;
  }

  /*
   * Added for JPA
   */
  private void setEmail(String email) {
    this.email = email;
  }
}