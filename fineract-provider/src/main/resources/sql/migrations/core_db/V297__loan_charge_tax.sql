ALTER TABLE `m_loan_charge`
	ADD COLUMN `tax_group_id` BIGINT(20) NULL DEFAULT NULL AFTER `is_active`;

ALTER TABLE `m_loan_charge`
	ADD COLUMN `amount_sans_tax` DECIMAL(19,6) NULL DEFAULT NULL AFTER `tax_group_id`;
	
ALTER TABLE `m_loan_charge`
	ADD COLUMN `tax_amount` DECIMAL(19,6) NULL DEFAULT NULL AFTER `amount_sans_tax`;
	

CREATE TABLE `m_loan_charge_tax_details` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`loan_charge_id` BIGINT(20) NOT NULL,
	`tax_component_id` BIGINT(20) NOT NULL,
	`amount` DECIMAL(19,6) NOT NULL DEFAULT '0.000000',
	PRIMARY KEY (`id`),
	INDEX `FK__m_loan_charges` (`loan_charge_id`),
	INDEX `FK__m_tax_component` (`tax_component_id`),
	CONSTRAINT `FK__m_loan_charges` FOREIGN KEY (`loan_charge_id`) REFERENCES `m_loan_charge` (`id`),
	CONSTRAINT `FK__m_tax_component` FOREIGN KEY (`tax_component_id`) REFERENCES `m_tax_component` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
;

	
CREATE TABLE `m_loan_charge_tax_details_paid_by` (
	`id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`loan_charge_paid_by_id` BIGINT(20) NOT NULL,
	`tax_component_id` BIGINT(20) NOT NULL,
	`amount` DECIMAL(19,6) NOT NULL DEFAULT '0.000000',
	PRIMARY KEY (`id`),
	INDEX `FK__m_loan_charge_tax_paid_by` (`loan_charge_paid_by_id`),
	INDEX `FK__m_tax_components` (`tax_component_id`),
	CONSTRAINT `FK__m_loan_charge_tax_paid_by` FOREIGN KEY (`loan_charge_paid_by_id`) REFERENCES `m_loan_charge_paid_by` (`id`),
	CONSTRAINT `FK__m_tax_components` FOREIGN KEY (`tax_component_id`) REFERENCES `m_tax_component` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB
;


	