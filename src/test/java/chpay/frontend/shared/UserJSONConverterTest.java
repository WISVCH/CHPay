package chpay.frontend.shared;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import chpay.DatabaseHandler.transactiondb.entities.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserJSONConverterTest {

  @Test
  @DisplayName("Should convert list of users to JSON strings")
  void usersToJSON_ShouldConvertUsersToJsonStrings() throws JsonProcessingException {
    // Arrange
    List<User> users = new ArrayList<>();
    User user1 = new User("testUser1", "test.test@mail.com", "WISVCH.11111");

    User user2 = new User("testUser2", "john.doe@mail.com", "WISVCH.222222");

    users.add(user1);
    users.add(user2);

    ObjectMapper mapper = new ObjectMapper();
    String expectedJson1 = mapper.writeValueAsString(user1);
    String expectedJson2 = mapper.writeValueAsString(user2);

    // Act
    List<String> result = UserJSONConverter.UsersToJSON(users);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(expectedJson1, result.get(0));
    assertEquals(expectedJson2, result.get(1));
  }

  @Test
  @DisplayName("Should return empty list when input list is empty")
  void usersToJSON_ShouldReturnEmptyListForEmptyInput() throws JsonProcessingException {
    // Arrange
    List<User> emptyList = new ArrayList<>();

    // Act
    List<String> result = UserJSONConverter.UsersToJSON(emptyList);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle null user in list")
  void usersToJSON_ShouldHandleNullUser() throws JsonProcessingException {
    // Arrange
    List<User> users = new ArrayList<>();
    users.add(null);

    // Act
    List<String> result = UserJSONConverter.UsersToJSON(users);

    // Assert
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  @DisplayName("Should handle null list")
  void usersToJSON_ShouldHandleNullList() throws JsonProcessingException {
    // Act
    List<String> result = UserJSONConverter.UsersToJSON(null);

    // Assert
    assertNotNull(result);
    assertEquals(0, result.size());
  }
}
