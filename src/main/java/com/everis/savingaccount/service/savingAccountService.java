package com.everis.savingaccount.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.everis.savingaccount.consumer.webclient;
import com.everis.savingaccount.dto.message;
import com.everis.savingaccount.map.customer;
import com.everis.savingaccount.model.movements;
import com.everis.savingaccount.model.savingAccount;
import com.everis.savingaccount.repository.savingAccountRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class savingAccountService {
	@Autowired
	savingAccountRepository repository;

	private final List<String> operations = Arrays.asList("Retiro", "Deposito", "Trasnferencia", "Comision");
	private static final int LIMIT_MOVEMENT = 5;

	private Boolean verifyCustomer(String id) {
		return webclient.customer.get().uri("/verifyId/{id}", id).retrieve().bodyToMono(Boolean.class).block();
	}

	private customer customerFind(String id) {
		return webclient.customer.get().uri("/{id}", id).retrieve().bodyToMono(customer.class).block();
	}

	private Boolean verifyNumberCC(String number) {
		return webclient.currentAccount.get().uri("/verifyByNumberAccount/" + number).retrieve()
				.bodyToMono(Boolean.class).block();
	}

	private Boolean verifyNumberSC(String number) {
		return webclient.savingAccount.get().uri("/verifyByNumberAccount/" + number).retrieve()
				.bodyToMono(Boolean.class).block();
	}

	private Boolean verifyNumberFC(String number) {
		return webclient.fixedAccount.get().uri("/verifyByNumberAccount/" + number).retrieve().bodyToMono(Boolean.class)
				.block();
	}

	private Boolean verifyCE(String number) {
		if (verifyNumberCC(number) || verifyNumberSC(number) || verifyNumberFC(number))
			return true;
		return false;
	}

	private Boolean verifyCR(String number) {
		if (verifyNumberCC(number) || verifyNumberSC(number) || verifyNumberFC(number))
			return true;
		return false;
	}

	private double getAmountByNumber(String number) {
		return repository.findByAccountNumber(number).getAmount();
	}

	private String addMovements(movements movement) {
		double val = getAmountByNumber(movement.getAccountEmisor());
		savingAccount model = repository.findByAccountNumber(movement.getAccountEmisor());

		if (movement.getType().equals("Deposito")) {
			model.setAmount(movement.getAmount() + val);
			model.getMovements().add(movement);
		} else {
			if (movement.getAmount() > val)
				return "Cantidad insuficiente.";
			else {

				if (movement.getType().equals("Trasnferencia") && movement.getAccountRecep() != null) {
					if (verifyCR(movement.getAccountRecep())) {
						if (verifyNumberCC(movement.getAccountRecep()))
							webclient.currentAccount.post().uri("/addTransfer")
									.body(Mono.just(movement), movements.class).retrieve().bodyToMono(Object.class)
									.subscribe();

						if (verifyNumberSC(movement.getAccountRecep()))
							webclient.savingAccount.post().uri("/addTransfer")
									.body(Mono.just(movement), movements.class).retrieve().bodyToMono(Object.class)
									.subscribe();

						if (verifyNumberFC(movement.getAccountRecep()))
							webclient.fixedAccount.post().uri("/addTransfer").body(Mono.just(movement), movements.class)
									.retrieve().bodyToMono(Object.class).subscribe();

					} else
						return "Cuenta receptora no exciste.";
				}

				model.setAmount(val - movement.getAmount());
				model.getMovements().add(movement);
			}
		}

		repository.save(model);
		return "Movimiento realizado";
	}

	private double getAmountByID(String id) {
		return repository.findById(id).get().getAmount();
	}

	private void addComisionById(String id) {
		double comision = 1.5;
		double amount = getAmountByID(id) - comision;
		if (amount >= 0) {
			savingAccount model = repository.findById(id).get();

			movements mobj = new movements(model.getAccountNumber(), "Comision", comision);
			model.getMovements().add(mobj);

			model.setAmount(amount);
			repository.save(model);
		}
	}

	public Mono<Object> save(savingAccount model) {
		String msg = "Cuenta creada.";

		if (verifyCustomer(model.getIdCustomer())) {
			String typeCustomer = customerFind(model.getIdCustomer()).getType();

			if (typeCustomer.equals("personal")) {
				if (!repository.existsByIdCustomer(model.getIdCustomer()))
					repository.save(model);
				else
					msg = "Usted ya no puede tener mas cuentas de ahorro.";
			} else
				msg = "Las cuentas empresariales no deben tener cuentas de ahorro.";
		} else
			msg = "Cliente no registrado";

		return Mono.just(new message(msg));
	}

	public Mono<Object> saveTransfer(movements model) {
		savingAccount obj = repository.findByAccountNumber(model.getAccountRecep());
		double amount = obj.getAmount();

		obj.setAmount(amount + model.getAmount());
		obj.getMovements().add(model);

		repository.save(obj);
		return Mono.just(new message(""));
	}

	public Mono<Object> saveMovements(movements model) {
		String msg = "Movimiento realizado";
		String idaccount = repository.findByAccountNumber(model.getAccountEmisor()).getIdSavingAccount();
		int movementcant = (int) repository.findByAccountNumber(model.getAccountEmisor()).getMovements().size();

		if (movementcant < LIMIT_MOVEMENT) {
			if (repository.existsByAccountNumber(model.getAccountEmisor())) {
				if (!operations.stream().filter(c -> c.equals(model.getType())).collect(Collectors.toList())
						.isEmpty()) {
					msg = addMovements(model);
					addComisionById(idaccount);
				} else
					msg = "Selecione una operacion correcta.";
			} else
				msg = "Numero de cuenta incorrecto.";
		} else
			msg = "Llego a su limite de movimientos.";

		return Mono.just(new message(msg));
	}

	public Flux<Object> getAll() {
		return Flux.fromIterable(repository.findAll());
	}

	public Mono<Object> getOne(String id) {
		return Mono.just(repository.findByAccountNumber(id));
	}

	public Mono<Boolean> _verifyByNumberAccount(String number) {
		return Mono.just(repository.existsByAccountNumber(number));
	}

	public Flux<Object> getByCustomer(String id) {
		return Flux.fromIterable(
				repository.findAll().stream().filter(c -> c.getIdCustomer().equals(id)).collect(Collectors.toList()));
	}
}
