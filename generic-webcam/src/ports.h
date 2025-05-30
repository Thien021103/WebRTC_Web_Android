#ifndef PORTS_H_
#define PORTS_H_

#include <stdlib.h>
#include "address.h"

int ports_resolve_addr(const char* host, Address* addr);

int ports_get_host_addr(Address* addr, const char* iface_prefix);

uint32_t ports_get_epoch_time();

#endif  // PORTS_H_
