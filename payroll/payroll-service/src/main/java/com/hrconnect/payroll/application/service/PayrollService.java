package com.hrconnect.payroll.application.service;

import com.hrconnect.payroll.domain.model.EmployeeSnapshot;
import com.hrconnect.payroll.domain.repository.EmployeeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayrollService {

  private final RestTemplate restTemplate = new RestTemplate();
  private final EmployeeSnapshotRepository employeeSnapshotRepository;

  public void calculer(String employeeId, int month, int year, String authHeader) {
    // Récupération du salaire de l'employé depuis la projection locale
    EmployeeSnapshot employeeSnapshot = employeeSnapshotRepository.findByReference(employeeId)
      .orElseThrow(() -> new IllegalArgumentException("Employé non trouvé: " + employeeId));

    BigDecimal salaireAnnuel = employeeSnapshot.getSalaireAnnuelBase();
    if (salaireAnnuel == null) {
      throw new IllegalArgumentException("Salaire non défini pour l'employé: " + employeeId);
    }

    // Calcul du salaire mensuel brut
    BigDecimal salaireMensuelBrut = salaireAnnuel.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    log.info("Salaire mensuel brut pour l'employé {} : {}", employeeId, salaireMensuelBrut);

    HttpHeaders headers = new HttpHeaders();
    if (StringUtils.hasText(authHeader)) {
      headers.set("Authorization", authHeader);
      log.debug("Propagation du token d'authentification vers le service leave");
    }
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
    ResponseEntity<List<LeaveDTO>> response = restTemplate.exchange(
      "http://localhost:9082/api/leaves/employee/" + employeeId,
      HttpMethod.GET,
      requestEntity,
      new ParameterizedTypeReference<List<LeaveDTO>>() {
      }
    );
    LocalDate start = LocalDate.of(year, month, 1);
    LocalDate end = LocalDate.of(year, month, 1).plusMonths(1);
    List<LeaveDTO> leaves = response.getBody();
    if (leaves == null) {
      leaves = List.of();
    }

    long joursSansSolde = leaves.stream()
      .filter(l -> l.getType() == LeaveType.SANS_SOLDE)
      .filter(l -> l.getStatut() == LeaveStatus.VALIDE)
      .mapToLong(l -> compterJoursDansPeriode(l, start, end))
      .sum();

    log.info("Leaves : {}", leaves);
    log.info("Jours sans solde dans la période {}/{} : {}", month, year, joursSansSolde);

    // Calcul de la déduction pour jours sans solde (base 22 jours ouvrés par mois)
    BigDecimal tauxJournalier = salaireMensuelBrut.divide(BigDecimal.valueOf(22), 2, RoundingMode.HALF_UP);
    BigDecimal deductionSansSolde = tauxJournalier.multiply(BigDecimal.valueOf(joursSansSolde));

    // Calcul du salaire net (simplifié, sans charges sociales pour l'exemple)
    BigDecimal salaireNet = salaireMensuelBrut.subtract(deductionSansSolde);

    log.info("Fiche de paie pour l'employé {} - {}/{}", employeeId, month, year);
    log.info("  Salaire mensuel brut : {} €", salaireMensuelBrut);
    log.info("  Jours sans solde : {}", joursSansSolde);
    log.info("  Déduction sans solde : {} €", deductionSansSolde);
    log.info("  Salaire net : {} €", salaireNet);
  }

  /**
   * Compte le nombre de jours d'un congé qui tombent dans la période [start, end[
   */
  private long compterJoursDansPeriode(LeaveDTO leave, LocalDate start, LocalDate end) {
    LocalDate debutEffectif = leave.getDateDebut().isBefore(start) ? start : leave.getDateDebut();
    LocalDate finEffective = leave.getDateFin().isBefore(end) ? leave.getDateFin() : end.minusDays(1);

    if (debutEffectif.isAfter(finEffective)) {
      return 0;
    }

    return Period.between(debutEffectif, finEffective).getDays() + 1;
  }


}
