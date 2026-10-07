<?php
/**
 * GET /wp-json/samupdater-app/v1/catalog
 */

namespace SamUpdater\AppApi;

use WP_REST_Request;
use WP_REST_Response;
use WP_REST_Server;

defined( 'ABSPATH' ) || exit;

final class Rest_Controller {

	const ROUTE_NAMESPACE = 'samupdater-app/v1';
	const CACHE_SECONDS   = 900;
	const RATE_LIMIT      = 60;
	const RATE_WINDOW     = 60;

	public static function register() {
		register_rest_route(
			self::ROUTE_NAMESPACE,
			'/catalog',
			array(
				'methods'             => WP_REST_Server::READABLE,
				'callback'            => array( __CLASS__, 'catalog' ),
				'permission_callback' => '__return_true',
			)
		);
	}

	public static function catalog( WP_REST_Request $request ) {
		if ( self::is_rate_limited() ) {
			return new WP_REST_Response(
				array(
					'code'    => 'samupdater_rate_limited',
					'message' => 'Too many requests. Try again in a minute.',
				),
				429,
				array( 'Retry-After' => (string) self::RATE_WINDOW )
			);
		}

		return new WP_REST_Response(
			Catalog_Store::get(),
			200,
			array( 'Cache-Control' => 'public, max-age=' . self::CACHE_SECONDS )
		);
	}

	/**
	 * Simple per-IP counter. Uses REMOTE_ADDR only, because forwarded headers can be faked.
	 * Behind Cloudflare or another proxy, restore the real IP at the server level, or raise the
	 * limit with the samupdater_app_api_rate_limit filter (0 turns it off).
	 */
	private static function is_rate_limited() {
		$limit = (int) apply_filters( 'samupdater_app_api_rate_limit', self::RATE_LIMIT );
		$ip    = isset( $_SERVER['REMOTE_ADDR'] ) ? sanitize_text_field( wp_unslash( $_SERVER['REMOTE_ADDR'] ) ) : '';
		if ( $limit <= 0 || '' === $ip ) {
			return false;
		}

		$key   = 'samupdater_app_rl_' . md5( $ip );
		$count = (int) get_transient( $key );
		if ( $count >= $limit ) {
			return true;
		}
		set_transient( $key, $count + 1, self::RATE_WINDOW );
		return false;
	}
}
