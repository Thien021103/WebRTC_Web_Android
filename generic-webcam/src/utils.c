#include "utils.h"
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "mbedtls/md.h"
#include "mbedtls/aes.h"

void utils_random_string(char* s, const int len) {
  int i;

  static const char alphanum[] =
      "0123456789"
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
      "abcdefghijklmnopqrstuvwxyz";

  srand(time(NULL));

  for (i = 0; i < len; ++i) {
    s[i] = alphanum[rand() % (sizeof(alphanum) - 1)];
  }

  s[len] = '\0';
}

void utils_get_hmac_sha1(const char* input, size_t input_len, const char* key, size_t key_len, unsigned char* output) {
  mbedtls_md_context_t ctx;
  mbedtls_md_type_t md_type = MBEDTLS_MD_SHA1;
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(md_type), 1);
  mbedtls_md_hmac_starts(&ctx, (const unsigned char*)key, key_len);
  mbedtls_md_hmac_update(&ctx, (const unsigned char*)input, input_len);
  mbedtls_md_hmac_finish(&ctx, output);
  mbedtls_md_free(&ctx);
}

void utils_get_md5(const char* input, size_t input_len, unsigned char* output) {
  mbedtls_md_context_t ctx;
  mbedtls_md_type_t md_type = MBEDTLS_MD_MD5;
  mbedtls_md_init(&ctx);
  mbedtls_md_setup(&ctx, mbedtls_md_info_from_type(md_type), 1);
  mbedtls_md_starts(&ctx);
  mbedtls_md_update(&ctx, (const unsigned char*)input, input_len);
  mbedtls_md_finish(&ctx, output);
  mbedtls_md_free(&ctx);
}

void utils_encrypt_aes_token(const char *input, const char *key, char *output) {
  mbedtls_aes_context aes;
  unsigned char key_bin[16]; // Use first 16 bytes of secret_key
  memcpy(key_bin, key, 16);
  mbedtls_aes_init(&aes);
  mbedtls_aes_setkey_enc(&aes, key_bin, 128);
  unsigned char iv[16] = {0}; // Fixed IV for simplicity
  mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_ENCRYPT, 36, iv, (unsigned char *)input, (unsigned char *)output);
  mbedtls_aes_free(&aes);
}
void utils_decrypt_aes_token(const char *input, const char *key, char *output) {
  mbedtls_aes_context aes;
  unsigned char key_bin[16];
  memcpy(key_bin, key, 16);
  mbedtls_aes_init(&aes);
  mbedtls_aes_setkey_dec(&aes, key_bin, 128);
  unsigned char iv[16] = {0};
  mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_DECRYPT, 36, iv, (unsigned char *)input, (unsigned char *)output);
  mbedtls_aes_free(&aes);
}

static const char base64_table[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

void base64_encode(const unsigned char* input, int input_len, char* output, int output_len) {
  int i, j;
  unsigned char buf[3];
  int buf_len;
  int output_index = 0;

  for (i = 0; i < input_len; i += 3) {
    buf_len = 0;
    for (j = 0; j < 3; j++) {
      if (i + j < input_len) {
        buf[j] = input[i + j];
        buf_len++;
      } else {
        buf[j] = 0;
      }
    }

    if (output_index + 4 > output_len) {
      return;
    }

    output[output_index++] = base64_table[(buf[0] & 0xFC) >> 2];
    output[output_index++] = base64_table[((buf[0] & 0x03) << 4) | ((buf[1] & 0xF0) >> 4)];
    output[output_index++] = (buf_len > 1) ? base64_table[((buf[1] & 0x0F) << 2) | ((buf[2] & 0xC0) >> 6)] : '=';
    output[output_index++] = (buf_len > 2) ? base64_table[buf[2] & 0x3F] : '=';
  }

  output[output_index] = '\0';
}

int base64_decode(const char* input, int input_len, unsigned char* output, int output_len) {
  int i, j;
  unsigned char buf[4];
  int buf_len;
  int output_index = 0;

  for (i = 0; i < input_len; i += 4) {
    buf_len = 0;
    for (j = 0; j < 4; j++) {
      if (i + j < input_len) {
        if (input[i + j] != '=') {
          buf[j] = strchr(base64_table, input[i + j]) - base64_table;
          buf_len++;
        } else {
          buf[j] = 0;
        }
      }
    }

    if (output_index + buf_len > output_len) {
      return -1;
    }

    output[output_index++] = (buf[0] << 2) | ((buf[1] & 0x30) >> 4);
    if (buf_len > 2) {
      output[output_index++] = ((buf[1] & 0x0F) << 4) | ((buf[2] & 0x3C) >> 2);
    }
    if (buf_len > 3) {
      output[output_index++] = ((buf[2] & 0x03) << 6) | buf[3];
    }
  }

  return output_index;
}