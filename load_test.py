#!/usr/bin/env python3
"""
Load‑test script for the *Meli Discount* REST endpoint.

Features
--------
* **Async & concurrent** HTTP requests via *httpx*.
* Accepts item IDs from CLI, file or synthetic generation.
* Parameterised batch size, concurrency and iterations.
* Prints mean, median, 95‑th percentile, min, max latencies in milliseconds.
* Optional CSV export for deeper analysis.

Usage examples
--------------
# 1. 50 requests, each with 1 000 synthetic IDs (batches of 200), 10 workers
python load_test.py \
  --url http://localhost:9090/meli_discount \
  --num-ids 1000 \
  --batch-size 200 \
  --concurrency 10 \
  --runs 50

# 2. Real IDs from a file, store raw times to times.csv
python load_test.py --ids-file my_ids.txt --runs 200 --csv times.csv
"""

import argparse
import asyncio
import csv
import statistics
import sys
import time
from pathlib import Path
from typing import List
from urllib.parse import quote

import httpx


async def request_once(client: httpx.AsyncClient, base_url: str, item_ids: List[str]) -> float:
    """Return elapsed seconds for a single HTTP GET."""
    ids_param = quote(",".join(item_ids), safe=",")
    url = f"{base_url}?item_ids={ids_param}"

    start = time.perf_counter()
    response = await client.get(url)
    elapsed = time.perf_counter() - start

    if response.status_code != 200:
        print(
            f"WARN: HTTP {response.status_code} taking {elapsed*1000:.2f} ms (payload {len(response.content)} B)",
            file=sys.stderr,
        )
    return elapsed


async def worker(
    name: str,
    client: httpx.AsyncClient,
    base_url: str,
    batches: List[List[str]],
    iterations: int,
    results: List[float],
):
    for _ in range(iterations):
        for batch in batches:
            results.append(await request_once(client, base_url, batch))


async def run_load_test(args):
    # Resolve item ID source -------------------------------------------------
    if args.ids_file:
        ids = [line.strip() for line in Path(args.ids_file).read_text().splitlines() if line.strip()]
    elif args.ids:
        ids = args.ids
    else:  # synthetic MLA0..MLA{n-1}
        ids = [f"MLA{i}" for i in range(args.num_ids)]

    if not ids:
        raise SystemExit("No item IDs provided.")

    # Split IDs into batches to keep URLs < 8 KB (safe for most servers)
    batches = [ids[i : i + args.batch_size] for i in range(0, len(ids), args.batch_size)]

    results: List[float] = []
    async with httpx.AsyncClient(timeout=args.timeout) as client:
        tasks = [
            worker(f"worker-{i}", client, args.url, batches, args.runs, results)
            for i in range(args.concurrency)
        ]
        await asyncio.gather(*tasks)

    return results


def print_summary(times: List[float]):
    milli = lambda s: s * 1000
    print("\n==== Performance summary ====")
    print(f"Requests : {len(times)}")
    print(f"Mean     : {milli(statistics.mean(times)):.2f} ms")
    print(f"Median   : {milli(statistics.median(times)):.2f} ms")
    p95 = statistics.quantiles(times, n=20, method="inclusive")[18]
    print(f"P95      : {milli(p95):.2f} ms")
    print(f"Min | Max: {milli(min(times)):.2f} ms | {milli(max(times)):.2f} ms")
    print("==============================\n")


def write_csv(path: str, values: List[float]):
    with open(path, "w", newline="") as fh:
        wr = csv.writer(fh)
        wr.writerow(["elapsed_ms"])
        wr.writerows([[v * 1000] for v in values])
    print(f"Raw latencies written to {path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Async load tester for the Meli Discount API")
    parser.add_argument("--url", default="http://localhost:9090/meli_discount", help="Base endpoint URL")
    parser.add_argument("--ids", nargs="*", help="Item IDs provided inline (space‑separated)")
    parser.add_argument("--ids-file", help="Path to text file with one item_id per line")
    parser.add_argument("--num-ids", type=int, default=0, help="Generate N synthetic MLA IDs if no list provided")
    parser.add_argument("--batch-size", type=int, default=100, help="IDs per request (keeps URL length reasonable)")
    parser.add_argument("--concurrency", type=int, default=10, help="Number of parallel async workers")
    parser.add_argument("--runs", type=int, default=10, help="Iterations per worker (total=concurrency×runs×batches)")
    parser.add_argument("--timeout", type=float, default=30.0, help="Per‑request timeout in seconds")
    parser.add_argument("--csv", help="Optional CSV file to dump raw latencies")
    args = parser.parse_args()

    if not any([args.ids, args.ids_file, args.num_ids]):
        parser.error("Provide --ids, --ids-file or --num-ids")

    all_times = asyncio.run(run_load_test(args))
    print_summary(all_times)

    if args.csv:
        write_csv(args.csv, all_times)