package com.dgi.recouvrement.config;

import com.dgi.recouvrement.service.dgi.ExpediteurEmails;
import com.dgi.recouvrement.service.dgi.RelanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

/**
 * Les deux réveils de la relance, réglés dans config/relance.properties :
 * - la campagne, selon relance.planification ;
 * - l'expédition des emails en file d'attente, toutes les relance.email.intervalle-expedition-secondes.
 */
@Configuration
@EnableScheduling
public class RelanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(RelanceScheduler.class);

    private final RelanceService relanceService;
    private final ExpediteurEmails expediteurEmails;
    private final RelanceProperties props;

    public RelanceScheduler(RelanceService relanceService, ExpediteurEmails expediteurEmails, RelanceProperties props) {
        this.relanceService = relanceService;
        this.expediteurEmails = expediteurEmails;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${relance.email.intervalle-expedition-secondes}", timeUnit = TimeUnit.SECONDS)
    public void expedierEmails() {
        expediteurEmails.expedier();
    }

    // ${...} = valeur lue dans config/relance.properties (aucune valeur de secours ici)
    @Scheduled(cron = "${relance.planification}", zone = "${relance.fuseau-horaire}")
    public void relanceAutomatique() {
        if (!props.isActive()) {
            log.info("Relance automatique désactivée (relance.active=false)");
            return;
        }
        relanceService.lancerCampagne("AUTOMATIQUE");
    }
}
