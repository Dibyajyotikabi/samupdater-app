<?php
/**
 * Validates and cleans catalog JSON so it always matches what the Android app expects.
 *
 * Field names are camelCase on purpose. They map one to one to the app's Catalog data class.
 */

namespace SamUpdater\AppApi;

defined( 'ABSPATH' ) || exit;

final class Catalog_Schema {

	const MAX_ITEMS     = 500;
	const MAX_TEXT      = 200;
	const MIN_ANDROID   = 8;
	const MAX_ANDROID   = 30;
	const MAX_UPGRADES  = 10;
	const MODEL_PREFIX  = '/^SM-[A-Z0-9]{1,12}$/';
	const CSC_CODE      = '/^[A-Z0-9]{3}$/';
	const BETA_STATUSES = array( 'open', 'closed', 'upcoming', 'ended' );
	const STAGES        = array( 'announced', 'testing', 'beta', 'rolling_out' );

	/**
	 * @param mixed $input Decoded JSON.
	 * @return array{catalog: array, errors: string[]}
	 */
	public static function sanitize( $input ) {
		if ( ! is_array( $input ) || array_values( $input ) === $input && ! empty( $input ) ) {
			return array(
				'catalog' => array(),
				'errors'  => array( 'The catalog must be a JSON object with devices, betaPrograms, upcoming and cscSuggestions.' ),
			);
		}

		$sections = array(
			'devices'        => array( __CLASS__, 'device' ),
			'betaPrograms'   => array( __CLASS__, 'beta_program' ),
			'upcoming'       => array( __CLASS__, 'upcoming' ),
			'cscSuggestions' => array( __CLASS__, 'csc_option' ),
		);

		$catalog = array();
		$errors  = array();
		foreach ( $sections as $key => $cleaner ) {
			$result          = self::list_of( $input[ $key ] ?? array(), $key, $cleaner );
			$catalog[ $key ] = $result['items'];
			$errors          = array_merge( $errors, $result['errors'] );
		}

		return array(
			'catalog' => $catalog,
			'errors'  => $errors,
		);
	}

	/**
	 * Runs a cleaner on every entry of a list. A cleaner returns the clean item, or a string with the problem.
	 */
	private static function list_of( $raw, $section, callable $cleaner ) {
		if ( ! is_array( $raw ) || array_values( $raw ) !== $raw ) {
			return array(
				'items'  => array(),
				'errors' => array( "$section must be a list." ),
			);
		}
		if ( count( $raw ) > self::MAX_ITEMS ) {
			return array(
				'items'  => array(),
				'errors' => array( "$section has more than " . self::MAX_ITEMS . ' entries.' ),
			);
		}

		$items  = array();
		$errors = array();
		foreach ( $raw as $index => $entry ) {
			$clean = is_array( $entry ) ? call_user_func( $cleaner, $entry ) : 'must be an object.';
			if ( is_string( $clean ) ) {
				$errors[] = "{$section}[{$index}]: $clean";
			} else {
				$items[] = $clean;
			}
		}

		return array(
			'items'  => $items,
			'errors' => $errors,
		);
	}

	private static function device( array $raw ) {
		$prefix   = strtoupper( self::text( $raw, 'modelPrefix' ) );
		$name     = self::text( $raw, 'name' );
		$launch   = self::int( $raw, 'launchAndroid' );
		$upgrades = self::int( $raw, 'osUpgrades' );
		$cscs     = self::codes( $raw, 'rolloutCscs' );

		if ( ! preg_match( self::MODEL_PREFIX, $prefix ) ) {
			return "modelPrefix \"$prefix\" should look like SM-S938.";
		}
		if ( '' === $name ) {
			return 'name is required.';
		}
		if ( null === $launch || $launch < self::MIN_ANDROID || $launch > self::MAX_ANDROID ) {
			return 'launchAndroid must be a number between ' . self::MIN_ANDROID . ' and ' . self::MAX_ANDROID . '.';
		}
		if ( null === $upgrades || $upgrades < 0 || $upgrades > self::MAX_UPGRADES ) {
			return 'osUpgrades must be a number between 0 and ' . self::MAX_UPGRADES . '.';
		}
		if ( is_string( $cscs ) ) {
			return $cscs;
		}

		return self::without_empty(
			array(
				'modelPrefix'   => $prefix,
				'name'          => $name,
				'launchAndroid' => $launch,
				'osUpgrades'    => $upgrades,
				'securityTier'  => self::text( $raw, 'securityTier' ),
				'securityUntil' => self::text( $raw, 'securityUntil' ),
				'rolloutCscs'   => $cscs,
			)
		);
	}

	private static function beta_program( array $raw ) {
		$one_ui   = self::text( $raw, 'oneUi' );
		$status   = strtolower( self::text( $raw, 'status' ) );
		$prefixes = self::strings( $raw, 'modelPrefixes', 'strtoupper' );

		if ( '' === $one_ui ) {
			return 'oneUi is required, for example "9".';
		}
		if ( ! in_array( $status, self::BETA_STATUSES, true ) ) {
			return 'status must be one of: ' . implode( ', ', self::BETA_STATUSES ) . '.';
		}
		if ( empty( $prefixes ) ) {
			return 'modelPrefixes needs at least one model prefix.';
		}
		foreach ( $prefixes as $prefix ) {
			if ( ! preg_match( self::MODEL_PREFIX, $prefix ) ) {
				return "modelPrefixes entry \"$prefix\" should look like SM-S938.";
			}
		}

		return self::without_empty(
			array(
				'oneUi'         => $one_ui,
				'status'        => $status,
				'modelPrefixes' => $prefixes,
				'countries'     => self::strings( $raw, 'countries' ),
				'latestBeta'    => strtoupper( self::text( $raw, 'latestBeta' ) ),
				'noticeUrl'     => self::url( $raw, 'noticeUrl' ),
				'updated'       => self::text( $raw, 'updated' ),
			)
		);
	}

	private static function upcoming( array $raw ) {
		$one_ui  = self::text( $raw, 'oneUi' );
		$stage   = strtolower( self::text( $raw, 'stage' ) );
		$android = self::int( $raw, 'android' );

		if ( '' === $one_ui ) {
			return 'oneUi is required, for example "9".';
		}
		if ( ! in_array( $stage, self::STAGES, true ) ) {
			return 'stage must be one of: ' . implode( ', ', self::STAGES ) . '.';
		}
		if ( isset( $raw['android'] ) && ( null === $android || $android < self::MIN_ANDROID || $android > self::MAX_ANDROID ) ) {
			return 'android must be a number between ' . self::MIN_ANDROID . ' and ' . self::MAX_ANDROID . '.';
		}

		return self::without_empty(
			array(
				'oneUi'   => $one_ui,
				'android' => $android,
				'stage'   => $stage,
				'note'    => self::text( $raw, 'note' ),
				'url'     => self::url( $raw, 'url' ),
			)
		);
	}

	private static function csc_option( array $raw ) {
		$code    = strtoupper( self::text( $raw, 'code' ) );
		$country = self::text( $raw, 'country' );

		if ( ! preg_match( self::CSC_CODE, $code ) ) {
			return "code \"$code\" must be three letters or digits, like INS.";
		}
		if ( '' === $country ) {
			return 'country is required.';
		}
		return array(
			'code'    => $code,
			'country' => $country,
		);
	}

	private static function text( array $raw, $key ) {
		return self::clean( $raw[ $key ] ?? null );
	}

	private static function clean( $value ) {
		return is_scalar( $value ) ? mb_substr( sanitize_text_field( (string) $value ), 0, self::MAX_TEXT ) : '';
	}

	private static function int( array $raw, $key ) {
		$value = $raw[ $key ] ?? null;
		return is_int( $value ) || ( is_string( $value ) && ctype_digit( $value ) ) ? (int) $value : null;
	}

	/** Only https links. Anything else is dropped. */
	private static function url( array $raw, $key ) {
		$url = self::text( $raw, $key );
		return '' === $url ? '' : esc_url_raw( $url, array( 'https' ) );
	}

	private static function strings( array $raw, $key, ?callable $transform = null ) {
		$list = isset( $raw[ $key ] ) && is_array( $raw[ $key ] ) ? $raw[ $key ] : array();
		$out  = array();
		foreach ( array_slice( $list, 0, self::MAX_ITEMS ) as $value ) {
			$clean = self::clean( $value );
			if ( '' !== $clean ) {
				$out[] = $transform ? $transform( $clean ) : $clean;
			}
		}
		return array_values( array_unique( $out ) );
	}

	/** @return string[]|string The CSC codes, or a problem description. */
	private static function codes( array $raw, $key ) {
		$codes = self::strings( $raw, $key, 'strtoupper' );
		foreach ( $codes as $code ) {
			if ( ! preg_match( self::CSC_CODE, $code ) ) {
				return "$key entry \"$code\" must be three letters or digits, like INS.";
			}
		}
		return $codes;
	}

	/** Drops blank optional fields so the app falls back to its own defaults. Zero stays. */
	private static function without_empty( array $item ) {
		return array_filter(
			$item,
			static function ( $value ) {
				return null !== $value && '' !== $value && array() !== $value;
			}
		);
	}
}
