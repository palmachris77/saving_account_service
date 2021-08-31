package com.everis.savingaccount.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.everis.savingaccount.consumer.webclient;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "saving-account")
public class savingAccount {
	@Id
	private String idSavingAccount;
	private String accountNumber = webclient.logic.get().uri("/generatedNumberLong/12").retrieve()
			.bodyToMono(String.class).block();

	private Date dateCreated = new Date();
	private double amount = 0.0;
	private List<movements> movements = new ArrayList<movements>();
	private String profile;
	private String typeAccount = "Cuenta de ahorro.";

	@NotBlank(message = "Debe seleccionar un cliente.")
	private String idCustomer;

	public savingAccount() {
		this.profile = "";
	}

	public savingAccount(String idCustomer) {
		this.profile = "";
		this.idCustomer = idCustomer;
	}

	public savingAccount(String profile, String idCustomer) {
		this.profile = profile;
		this.idCustomer = idCustomer;
	}
}
