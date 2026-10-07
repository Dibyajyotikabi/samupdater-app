<?php
/**
 * Plain PHP checks for Catalog_Schema. Run with: php tests/catalog-schema-test.php
 * WordPress functions the schema uses are stubbed below.
 */

define( 'ABSPATH', __DIR__ );

function sanitize_text_field( $text ) {
	return trim( preg_replace( '/\s+/', ' ', strip_tags( $text ) ) );
}

function esc_url_raw( $url, $protocols ) {
	$scheme = strtolower( (string) parse_url( $url, PHP_URL_SCHEME ) );
	return in_array( $scheme, $protocols, true ) ? $url : '';
}

require __DIR__ . '/../includes/class-catalog-schema.php';

use SamUpdater\AppApi\Catalog_Schema;

$failures = 0;
function check( $name, $condition ) {
	global $failures;
	echo ( $condition ? 'PASS ' : 'FAIL ' ) . $name . PHP_EOL;
	$failures += $condition ? 0 : 1;
}

$seed = Catalog_Schema::sanitize( json_decode( file_get_contents( __DIR__ . '/../data/catalog-seed.json' ), true ) );
check( 'bundled seed is valid', array() === $seed['errors'] );
check( 'seed keeps every device', 24 === count( $seed['catalog']['devices'] ) );
check( 'seed keeps every CSC', 31 === count( $seed['catalog']['cscSuggestions'] ) );

$good = Catalog_Schema::sanitize(
	array(
		'devices'      => array(
			array( 'modelPrefix' => 'sm-s938', 'name' => '<b>Galaxy S25 Ultra</b>', 'launchAndroid' => 15, 'osUpgrades' => 0, 'securityTier' => '', 'rolloutCscs' => array( 'ins', 'EUX', 'INS' ) ),
		),
		'betaPrograms' => array(
			array( 'oneUi' => '9', 'status' => 'Open', 'modelPrefixes' => array( 'SM-S93' ), 'noticeUrl' => 'javascript:alert(1)' ),
		),
		'upcoming'     => array( array( 'oneUi' => '9', 'stage' => 'testing', 'android' => '17' ) ),
		'extra'        => 'ignored',
	)
);
$device = $good['catalog']['devices'][0];
check( 'valid catalog has no errors', array() === $good['errors'] );
check( 'model prefix is uppercased', 'SM-S938' === $device['modelPrefix'] );
check( 'HTML is stripped', 'Galaxy S25 Ultra' === $device['name'] );
check( 'zero upgrades is kept', 0 === $device['osUpgrades'] );
check( 'blank optional fields are dropped', ! array_key_exists( 'securityTier', $device ) );
check( 'CSC codes are uppercased and deduplicated', array( 'INS', 'EUX' ) === $device['rolloutCscs'] );
check( 'status is lowercased', 'open' === $good['catalog']['betaPrograms'][0]['status'] );
check( 'non-https URLs are dropped', ! isset( $good['catalog']['betaPrograms'][0]['noticeUrl'] ) );
check( 'numeric strings become ints', 17 === $good['catalog']['upcoming'][0]['android'] );
check( 'unknown keys are not passed through', ! isset( $good['catalog']['extra'] ) );
check( 'missing sections become empty lists', array() === $good['catalog']['cscSuggestions'] );
check( 'lists encode as JSON arrays', '[]' === json_encode( $good['catalog']['cscSuggestions'] ) );

$bad = Catalog_Schema::sanitize(
	array(
		'devices'        => array(
			array( 'modelPrefix' => 'S938', 'name' => 'x', 'launchAndroid' => 15, 'osUpgrades' => 7 ),
			array( 'modelPrefix' => 'SM-S938', 'name' => 'x', 'launchAndroid' => 99, 'osUpgrades' => 7 ),
			array( 'modelPrefix' => 'SM-S938', 'name' => 'x', 'launchAndroid' => 15, 'osUpgrades' => 7, 'rolloutCscs' => array( 'INDIA' ) ),
			'not an object',
		),
		'betaPrograms'   => array( array( 'oneUi' => '9', 'status' => 'maybe', 'modelPrefixes' => array( 'SM-S93' ) ) ),
		'upcoming'       => array( array( 'oneUi' => '9', 'stage' => 'rumored' ) ),
		'cscSuggestions' => array( 'code' => 'INS' ),
	)
);
check( 'every bad entry is reported', 7 === count( $bad['errors'] ) );
check( 'errors name the entry', 0 === strpos( $bad['errors'][0], 'devices[0]:' ) );
check( 'bad entries are not saved', array() === $bad['catalog']['devices'] );

check( 'a JSON list is rejected', 1 === count( Catalog_Schema::sanitize( array( 1, 2 ) )['errors'] ) );
check( 'a string is rejected', 1 === count( Catalog_Schema::sanitize( 'x' )['errors'] ) );

echo PHP_EOL . ( $failures ? "$failures check(s) failed" : 'All checks passed' ) . PHP_EOL;
exit( $failures ? 1 : 0 );
