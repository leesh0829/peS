package com.pes.user.api;

import java.util.UUID;

import com.pes.user.domain.UserAccount;

public final class WorkerDtos {

	private WorkerDtos() {
	}

	public record Response(UUID id, String username, String displayName) {
		public static Response from(UserAccount worker) {
			return new Response(worker.getId(), worker.getUsername(), worker.getDisplayName());
		}
	}
}
