#if canImport(Testing)
import Testing
import Memmap2

@Suite("Memmap2 Swift Export Tests")
struct Memmap2ExportTests {
    @Test("Memmap2 swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "Memmap2 swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Memmap2

final class Memmap2ExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Memmap2 swift module imported cleanly")
    }
}
#endif
