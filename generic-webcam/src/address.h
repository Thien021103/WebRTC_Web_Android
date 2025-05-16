#ifndef ADDRESS_H_
#define ADDRESS_H_

#include "config.h"
#include <arpa/inet.h>
#include <sys/socket.h>
#include <stdint.h>

#define ADDRSTRLEN INET6_ADDRSTRLEN

typedef struct Address {
  uint8_t family;
  struct sockaddr_in sin;
  struct sockaddr_in6 sin6;
  uint16_t port;
} Address;

void addr_set_family(Address* addr, int family);

void addr_set_port(Address* addr, uint16_t port);

int addr_to_string(const Address* addr, char* buf, size_t len);

int addr_from_string(const char* str, Address* addr);

#endif  // ADDRESS_H_
