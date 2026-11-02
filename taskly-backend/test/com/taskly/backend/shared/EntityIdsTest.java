package com.taskly.backend.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskly.backend.shared.domain.EntityIds;
import com.taskly.backend.shared.domain.ValidationException;
import java.util.UUID;
import org.junit.Test;

public class EntityIdsTest {

	@Test
	public void generatesValidIds() {
		assertThat(EntityIds.isValid(EntityIds.generate().toString())).isTrue();
	}

	@Test
	public void parsesAndLowercasesIds() {
		UUID id = EntityIds.parse("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "task");
		assertThat(id).hasToString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	}

	@Test
	public void rejectsMalformedIdsWithLabel() {
		assertThatThrownBy(() -> EntityIds.parse("nope", "user"))
				.isInstanceOf(ValidationException.class)
				.hasMessage("Invalid user ID format");
		assertThatThrownBy(() -> EntityIds.parse(null, "task")).isInstanceOf(ValidationException.class);
		assertThatThrownBy(() -> EntityIds.parse(42, "task")).isInstanceOf(ValidationException.class);
	}
}
