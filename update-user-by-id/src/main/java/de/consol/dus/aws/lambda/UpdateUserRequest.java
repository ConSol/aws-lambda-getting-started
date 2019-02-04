package de.consol.dus.aws.lambda;

import java.util.Optional;

/**
 * A change request for a specific user, identified by id.
 *
 * @author Marco Bungart
 */
public class UpdateUserRequest {
  private long id;
  private String name;
  private String email;

  public long getId() {
    return id;
  }

  /*
   * Added for JSON De/Serialization
   */
  protected void setId(long id) {
    this.id = id;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  /*
   * Added for JSON De/Serialization
   */
  protected void setName(String name) {
    this.name = name;
  }

  public Optional<String> getEmail() {
    return Optional.ofNullable(email);
  }

  /*
   * Added for JSON De/Serialization
   */
  protected void setEmail(String email) {
    this.email = email;
  }
}